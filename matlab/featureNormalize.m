function [X_norm, mu, sigma] = featureNormalize(X)

X_norm = X;
mu = zeros(1, size(X, 2));
sigma = zeros(1, size(X, 2));

% set mu
mu = mean(X);
% set sigma
sigma = std(X);

% check sigma
for i = 1:length(sigma)
    divider = sigma(i);
    if divider == 0
        sigma(i) = max(X(:, i));
    end
end

for i = 1:size(X, 1)
    for j = 1:size(X, 2)
        divider = sigma(1, j);

        if divider ~= 0
            x_t = (X_norm(i, j) - mu(1, j));
            X_norm(i, j) = x_t / divider;
        end
    end
end

end