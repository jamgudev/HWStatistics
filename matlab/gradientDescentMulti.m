function [theta, J_history] = gradientDescentMulti(X, y, theta, alpha, num_iters, lambda)

m = length(y);
J_history = zeros(num_iters, 1);
for i = 1:num_iters
    h = X * theta;
    theta_one = theta;
    theta_one(1) = 0;
    % theta = theta - (alpha / m) * (X' * (h - y));
    theta = theta - (alpha / m) * (X' * (h - y) + lambda .* theta_one);   % dimension = n + 1 x 1

    J_history(i) = computeCostMulti(X, y, theta, lambda);
end

end
